// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract FundTransfer {
    // Event to emit when a transfer occurs
    event Transfer(address indexed from, address indexed to, uint256 amount);
    
    
    function sendFunds(address payable _recipient) public payable {
        require(_recipient != address(0), "Invalid recipient address");
        require(msg.value > 0, "Amount to send must be greater than 0");
        
    
        (bool sent, ) = _recipient.call{value: msg.value}("");
        require(sent, "Failure in sending funds");
        
        
        emit Transfer(msg.sender, _recipient, msg.value);
    }
    
    // this function checks the contract balance so as to confirm that funds were actually sent
    function getBalance() public view returns (uint256) {
        return address(this).balance;
    }
    
    
    receive() external payable {}
