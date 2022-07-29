package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;

import io.ebean.DB;
import io.ebean.Transaction;

import io.javalin.Javalin;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AppTest {

    private static Javalin app;
    private static String baseUrl;
    private static Url existingUrl;
    private static Transaction transaction;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        existingUrl = new Url("https://www.example.com");
        existingUrl.save();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
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
            String createdAt = formatter.format(existingUrl.getCreatedAt());

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
        }

        @Test
        void showUrlTest() {
            String id = String.valueOf(existingUrl.getId());

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls/" + id).asString();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains(existingUrl.getName());
        }
    }
}
