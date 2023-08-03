package com.example.nginxloadbalancing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

  @GetMapping("/")
  public ResponseEntity<String> healthCheck () {
    return ResponseEntity.status(HttpStatus.OK).body("healty");
  }

  @PostMapping("/test-api")
  public ResponseEntity<String> test () {
    return ResponseEntity.status(HttpStatus.OK).body("test success");
  }

  @PostMapping("/test-api2")
  public ResponseEntity<String> test2 () {
    return ResponseEntity.status(HttpStatus.OK).body("test 2 success");
  }

}
