package hexlet.code;

import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlController;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import io.javalin.plugin.rendering.template.JavalinThymeleaf;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import io.javalin.Javalin;

final class App {

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "8080");
        return Integer.parseInt(port);
    }

    private static String getMode() {
        return System.getenv().getOrDefault("APP_ENV", "development");
    }

    private static boolean isProduction() {
        return getMode().equals("production");
    }

    private static TemplateEngine getTemplateEngine() {
        ClassLoaderTemplateResolver classLoaderTemplateResolver = new ClassLoaderTemplateResolver();
        classLoaderTemplateResolver.setTemplateMode(TemplateMode.HTML);
        classLoaderTemplateResolver.setPrefix("/templates/");

        TemplateEngine templateEngine = new TemplateEngine();

        templateEngine.addTemplateResolver(classLoaderTemplateResolver);
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());

        return templateEngine;
    }

    private static void addRoutes(Javalin app) {
        app.get("/", RootController.welcome);

        app.routes(() -> {
            path("urls", () -> {
                get(UrlController.listUrls);
                post(UrlController.checkNewUrl);

                path("{id}", () -> {
                    get(UrlController.showUrl);
                    post("checks", UrlController.checkExistingUrl);
                });
            });
        });
    }

    static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            if (!isProduction()) {
                config.enableDevLogging();
            }
            config.enableWebjars();
            JavalinThymeleaf.configure(getTemplateEngine());
        });

        addRoutes(app);

        app.before(ctx -> {
            ctx.attribute("ctx", ctx);
        });

        return app;
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }
}
