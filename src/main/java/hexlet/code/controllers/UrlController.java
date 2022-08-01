package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;

import io.ebean.PagedList;

import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.List;
import java.util.stream.IntStream;

public final class UrlController {

    public static Handler listUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedList = new QUrl()
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy().id.asc()
                .findPagedList();

        List<Url> urls = pagedList.getList();
        List<UrlCheck> lastUrlChecks = urls.stream()
                .map(Url::getUrlChecks)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(list.size() - 1)).
                toList();

        int currentPage = pagedList.getPageIndex() + 1;
        int totalPages = pagedList.getTotalPageCount() + 1;

        List<Integer> pages = IntStream
                .range(1, totalPages)
                .boxed().toList();

        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.attribute("urls", urls);
        ctx.attribute("lastUrlChecks", lastUrlChecks);
        ctx.render("urls/index.html");
    };

    public static Handler checkNewUrl = ctx -> {
        String url = ctx.formParam("url");

        URL urlNet;
        try {
            urlNet = new URL(url);
        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }

        String urlName = urlNet.getProtocol() + "://" + urlNet.getAuthority();

        Url urlModel = new QUrl()
                .name.iequalTo(urlName)
                .findOne();

        if (urlModel != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "warning");
            ctx.redirect("/urls");
            return;
        }

        Url newUrlModel = new Url(urlName);
        newUrlModel.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect("/urls");
    };

    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url urlModel = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (urlModel == null) {
            throw new NotFoundResponse();
        }

        List<UrlCheck> urlChecks = new QUrlCheck()
                .url.id.equalTo(id)
                .id.desc().findList();

        ctx.attribute("urlChecks", urlChecks);
        ctx.attribute("url", urlModel);
        ctx.render("urls/show.html");
    };

    public static Handler checkExistingUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url urlModel = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (urlModel == null) {
            throw new NotFoundResponse();
        }

        HttpResponse<String> response = Unirest.get(urlModel.getName()).asString();
        int statusCode = response.getStatus();

        Document doc = Jsoup.parse(response.getBody());
        String title = doc.title();

        Element h1Elem = doc.selectFirst("h1");
        Element descriptionElem = doc.selectFirst("meta[name=description]");
        String h1 = (h1Elem != null) ? h1Elem.text() : "";
        String description = (descriptionElem != null) ? descriptionElem.attr("content") : "";

        UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, urlModel);
        urlCheck.save();

        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect("/urls/" + id);
    };
}
