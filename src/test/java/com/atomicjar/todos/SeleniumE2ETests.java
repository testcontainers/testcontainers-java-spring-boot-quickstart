package com.atomicjar.todos;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:14-alpine:///todos"
})
public class SeleniumE2ETests {

    @LocalServerPort
    private Integer localPort;

    static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
            .withAccessToHost(true)
            .withCapabilities(new ChromeOptions());

    @BeforeAll
    static void beforeAll() {
        chrome.start();
    }

    @AfterAll
    static void afterAll() {
        chrome.stop();
    }

    @Test
    void testAddTodo() {
        RemoteWebDriver driver = chrome.getWebDriver();
        Testcontainers.exposeHostPorts(localPort);
        String baseUrl = "http://host.testcontainers.internal:" + localPort;
        String apiUrl = baseUrl + "/todos";
        driver.get(baseUrl + "/?" + apiUrl);

        assertThat(driver.getTitle()).isEqualTo("Todo-Backend client");

        driver.findElement(By.id("new-todo")).sendKeys("first todo" + Keys.RETURN);
        WebElement element = driver.findElement(By.xpath("//*[@id=\"todo-list\"]/li[1]/div/label"));
        assertThat(element.getText()).isEqualTo("first todo");
    }
}
