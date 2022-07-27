package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;

import io.ebean.PagedList;

import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

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

        int currentPage = pagedList.getPageIndex() + 1;
        int totalPages = pagedList.getTotalPageCount() + 1;

        List<Integer> pages = IntStream
                .range(1, totalPages)
                .boxed().toList();

        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.attribute("urls", urls);
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
            ctx.redirect("urls");
            return;
        }

        Url newUrlModel = new Url(urlName);
        newUrlModel.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect("urls");
    };

    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url urlModel = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (urlModel == null) {
            throw new NotFoundResponse();
        }

        ctx.attribute("url", urlModel);
        ctx.render("urls/show.html");
    };

    public static Handler checkExistingUrl = ctx -> {
        ctx.redirect("urls/"); // ID
    };
}
