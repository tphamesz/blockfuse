package com.footballprediction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootApplication
@EnableCaching
public class FootballPredictionApplication {
    public static void main(String[] args) {
        SpringApplication.run(FootballPredictionApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// WebSocket Configuration
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
}

// Models
@Entity
@Data
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private int points;
}

@Entity
@Data
class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchTime;
    private Integer homeScore;
    private Integer awayScore;
    private boolean finished;
}

@Entity
@Data
class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User user;
    @ManyToOne
    private Match match;
    private Integer predictedHomeScore;
    private Integer predictedAwayScore;
    private int points;
}

@Entity
@Data
class League {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @ManyToMany
    private Set<User> users = new HashSet<>();
}

// Repositories
@Repository
interface UserRepository extends JpaRepository<User, Long> {}

@Repository
interface MatchRepository extends JpaRepository<Match, Long> {}

@Repository
interface PredictionRepository extends JpaRepository<Prediction, Long> {}

@Repository
interface LeagueRepository extends JpaRepository<League, Long> {}

// Services
@Service
@RequiredArgsConstructor
class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
}

@Service
@RequiredArgsConstructor
class MatchService {
    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;

    @Cacheable("upcomingMatches")
    public List<Match> getUpcomingMatches() {
        return matchRepository.findAll().stream()
            .filter(match -> !match.isFinished())
            .collect(Collectors.toList());
    }

    public void updateMatchResult(Long matchId, int homeScore, int awayScore) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found"));
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setFinished(true);
        matchRepository.save(match);
        calculatePredictionPoints(match);
    }

    private void calculatePredictionPoints(Match match) {
        List<Prediction> predictions = predictionRepository.findByMatch(match);
        for (Prediction prediction : predictions) {
            int points = calculatePoints(prediction, match);
            prediction.setPoints(points);
            predictionRepository.save(prediction);
        }
    }

    private int calculatePoints(Prediction prediction, Match match) {
        if (prediction.getPredictedHomeScore() == match.getHomeScore() 
            && prediction.getPredictedAwayScore() == match.getAwayScore()) {
            return 3; // Exact score
        } else if ((prediction.getPredictedHomeScore() > prediction.getPredictedAwayScore() 
                   && match.getHomeScore() > match.getAwayScore())
                   || (prediction.getPredictedHomeScore() < prediction.getPredictedAwayScore() 
                   && match.getHomeScore() < match.getAwayScore())
                   || (prediction.getPredictedHomeScore() == prediction.getPredictedAwayScore() 
                   && match.getHomeScore() == match.getAwayScore())) {
            return 1; // Correct result
        }
        return 0;
    }
}

// Controllers
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
class MatchController {
    private final MatchService matchService;

    @GetMapping("/upcoming")
    public ResponseEntity<List<Match>> getUpcomingMatches() {
        return ResponseEntity.ok(matchService.getUpcomingMatches());
    }

    @PostMapping("/{matchId}/result")
    public ResponseEntity<Void> updateMatchResult(@PathVariable Long matchId, 
                                                @RequestParam int homeScore, 
                                                @RequestParam int awayScore) {
        matchService.updateMatchResult(matchId, homeScore, awayScore);
        return ResponseEntity.ok().build();
    }
}

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
class PredictionController {
    private final PredictionRepository predictionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<Prediction> submitPrediction(@RequestBody Prediction prediction) {
        Prediction saved = predictionRepository.save(prediction);
        messagingTemplate.convertAndSend("/topic/predictions", saved);
        return ResponseEntity.ok(saved);
    }
}

// Exception Handling
@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("An error occurred: " + e.getMessage());
    }
}
