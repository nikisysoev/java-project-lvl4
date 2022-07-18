package hexlet.code;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;

public final class App {
    private static int getPort() {
        String port = System.getenv("PORT");
        if (port != null) {
            return Integer.parseInt(port);
        }
        return 5050;
    }

    public static Javalin getApp() {
        Javalin app = Javalin.create(JavalinConfig::enableDevLogging);
        app.get("/", ctx -> ctx.result("Hello, World!"));

        return app;
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }
}
