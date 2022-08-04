package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;

import io.ebean.DB;
import io.ebean.Transaction;

import io.javalin.Javalin;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

final class AppTest {

    private static Javalin app;

    private static String baseUrl;

    private static Url existingUrl;

    private static UrlCheck existingUrlCheck;

    private static Transaction transaction;

    private static MockWebServer mockWebServer;


    @BeforeAll
    static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        mockWebServer = new MockWebServer();
        mockWebServer.start(0);
        String mockUrl = mockWebServer.url("/example.com").toString();

        existingUrl = new Url(mockUrl);
        existingUrl.save();

        existingUrlCheck = new UrlCheck(200, "title", "h1", "description", existingUrl);
        existingUrlCheck.save();
    }

    @AfterAll
    static void afterAll() throws IOException {
        app.stop();
        mockWebServer.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Nested
    class RootControllerTest {

        @Test
        void testWelcome() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Анализатор страниц");
            assertThat(response.getBody()).contains("Проверить");
        }
    }

    @Nested
    class UrlControllerTest {

        @Test
        void testListUrls() {
            DateTimeFormatter formatter = DateTimeFormatter.
                    ofPattern("dd/MM/yyyy HH:mm").
                    withZone(ZoneId.systemDefault());
            String createdAt = formatter.format(existingUrlCheck.getCreatedAt());

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains(existingUrl.getName());
            assertThat(response.getBody()).contains(createdAt);
        }

        @Test
        void checkNewUrl() {
            String urlName = "https://www.google.com";

            HttpResponse responsePost = Unirest.post(baseUrl + "/urls")
                    .field("url", urlName)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains(urlName);
            assertThat(response.getBody()).contains("Страница успешно добавлена");

            Url url = new QUrl()
                    .name.iequalTo(urlName)
                    .findOne();

            assertThat(url).isNotNull();
            assertThat(url.getName()).isEqualTo(urlName);

            HttpResponse responsePost2 = Unirest.post(baseUrl + "/urls")
                    .field("url", "errorUrl")
                    .asEmpty();

            HttpResponse<String> response2 = Unirest.get(baseUrl).asString();

            assertThat(response2.getBody()).contains("Некорректный URL");
        }

        @Test
        void showUrlTest() {
            String id = String.valueOf(existingUrl.getId());

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls/" + id).asString();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains(existingUrl.getName());
        }

        @Test
        void checkExistingUrlTest() throws IOException {
            String body = Files.readString(Path.of("src/test/resources/UrlTest.html"));
            mockWebServer.enqueue(new MockResponse().setBody(body));

            String id = String.valueOf(existingUrl.getId());
            HttpResponse responsePost = Unirest.post(baseUrl + "/urls/" + id + "/checks").asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls/" + id);

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls/" + id).asString();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Добро пожаловать!");
            assertThat(response.getBody()).contains("Страница успешно проверена");

            UrlCheck urlCheck = new QUrlCheck()
                    .h1.iequalTo("Добро пожаловать!")
                    .findOne();

            assertThat(urlCheck).isNotNull();
            assertThat(urlCheck.getUrl().getName()).isEqualTo(existingUrl.getName());
            assertThat(urlCheck.getH1()).isEqualTo("Добро пожаловать!");
            assertThat(urlCheck.getTitle()).isEqualTo("Hexlet");
            assertThat(urlCheck.getDescription()).isEqualTo("Обучение");
        }
    }
}
