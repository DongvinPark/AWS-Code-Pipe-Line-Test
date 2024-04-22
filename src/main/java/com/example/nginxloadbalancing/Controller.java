package com.example.nginxloadbalancing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class Controller {

  @Value("${test.string}")
  private String testValueFromInjectedYml;

  @GetMapping("/")
  public ResponseEntity<String> healthCheck () {
    return ResponseEntity.status(HttpStatus.OK).body("healty");
  }

  @GetMapping("/test-api")
  public ResponseEntity<String> test () {
    return ResponseEntity.status(HttpStatus.OK).body(testValueFromInjectedYml + " and my new TEXT from new github Commit!!");
  }

}
