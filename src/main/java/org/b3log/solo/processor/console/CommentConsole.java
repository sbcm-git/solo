/*
 * Solo - A small and beautiful blogging system written in Java.
 * Copyright (c) 2010-2018, b3log.org & hacpai.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.b3log.solo.processor.console;

import org.b3log.latke.Keys;
import org.b3log.latke.Latkes;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.servlet.HttpMethod;
import org.b3log.latke.servlet.RequestContext;
import org.b3log.latke.servlet.annotation.Before;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.JsonRenderer;
import org.b3log.solo.model.Comment;
import org.b3log.solo.service.CommentMgmtService;
import org.b3log.solo.service.CommentQueryService;
import org.b3log.solo.util.Solos;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Comment console request processing.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.4, Dec 3, 2018
 * @since 0.4.0
 */
@RequestProcessor
@Before(ConsoleAuthAdvice.class)
public class CommentConsole {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CommentConsole.class);

    /**
     * Comment query service.
     */
    @Inject
    private CommentQueryService commentQueryService;

    /**
     * Comment management service.
     */
    @Inject
    private CommentMgmtService commentMgmtService;

    /**
     * Language service.
     */
    @Inject
    private LangPropsService langPropsService;

    /**
     * Removes a comment of an article by the specified request.
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "msg": ""
     * }
     * </pre>
     * </p>
     *
     * @param context the specified http request context
     */
    @RequestProcessing(value = "/console/page/comment/{id}", method = HttpMethod.DELETE)
    public void removePageComment(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final HttpServletRequest request = context.getRequest();
            final HttpServletResponse response = context.getResponse();
            final String commentId = context.pathVar("id");
            final JSONObject currentUser = Solos.getCurrentUser(request, response);
            if (!commentQueryService.canAccessComment(commentId, currentUser)) {
                ret.put(Keys.STATUS_CODE, false);
                ret.put(Keys.MSG, langPropsService.get("forbiddenLabel"));

                return;
            }

            commentMgmtService.removePageComment(commentId);

            ret.put(Keys.STATUS_CODE, true);
            ret.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            ret.put(Keys.STATUS_CODE, false);
            ret.put(Keys.MSG, langPropsService.get("removeFailLabel"));
        }
    }

    /**
     * Removes a comment of an article by the specified request.
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "msg": ""
     * }
     * </pre>
     * </p>
     *
     * @param context the specified http request context
     */
    @RequestProcessing(value = "/console/article/comment/{id}", method = HttpMethod.DELETE)
    public void removeArticleComment(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final HttpServletRequest request = context.getRequest();
            final HttpServletResponse response = context.getResponse();
            final String commentId = context.pathVar("id");
            final JSONObject currentUser = Solos.getCurrentUser(request, response);
            if (!commentQueryService.canAccessComment(commentId, currentUser)) {
                ret.put(Keys.STATUS_CODE, false);
                ret.put(Keys.MSG, langPropsService.get("forbiddenLabel"));

                return;
            }

            commentMgmtService.removeArticleComment(commentId);

            ret.put(Keys.STATUS_CODE, true);
            ret.put(Keys.MSG, langPropsService.get("removeSuccLabel"));
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            ret.put(Keys.STATUS_CODE, false);
            ret.put(Keys.MSG, langPropsService.get("removeFailLabel"));
        }
    }

    /**
     * Gets comments by the specified request.
     * <p>
     * The request URI contains the pagination arguments. For example, the
     * request URI is /console/comments/1/10/20, means the current page is 1, the
     * page size is 10, and the window size is 20.
     * </p>
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "pagination": {
     *         "paginationPageCount": 100,
     *         "paginationPageNums": [1, 2, 3, 4, 5]
     *     },
     *     "comments": [{
     *         "oId": "",
     *         "commentTitle": "",
     *         "commentName": "",
     *         "commentEmail": "",
     *         "thumbnailUrl": "",
     *         "commentURL": "",
     *         "commentContent": "",
     *         "commentTime": long,
     *         "commentSharpURL": ""
     *      }, ....]
     * }
     * </pre>
     * </p>
     *
     * @param context the specified http request context
     */
    @RequestProcessing(value = "/console/comments/{page}/{pageSize}/{windowSize}", method = HttpMethod.GET)
    public void getComments(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);

        try {
            final HttpServletRequest request = context.getRequest();
            final String requestURI = request.getRequestURI();
            final String path = requestURI.substring((Latkes.getContextPath() + "/console/comments/").length());

            final JSONObject requestJSONObject = Solos.buildPaginationRequest(path);
            final JSONObject result = commentQueryService.getComments(requestJSONObject);

            result.put(Keys.STATUS_CODE, true);

            renderer.setJSONObject(result);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            final JSONObject jsonObject = new JSONObject().put(Keys.STATUS_CODE, false);
            renderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
        }
    }

    /**
     * Gets comments of an article specified by the article id for administrator.
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "comments": [{
     *         "oId": "",
     *         "commentName": "",
     *         "commentEmail": "",
     *         "thumbnailUrl": "",
     *         "commentURL": "",
     *         "commentContent": "",
     *         "commentTime": long,
     *         "commentSharpURL": "",
     *         "isReply": boolean
     *      }, ....]
     * }
     * </pre>
     * </p>
     *
     * @param context the specified http request context
     */
    @RequestProcessing(value = "/console/comments/article/{id}", method = HttpMethod.GET)
    public void getArticleComments(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final String articleId = context.pathVar("id");
            final List<JSONObject> comments = commentQueryService.getComments(articleId);

            ret.put(Comment.COMMENTS, comments);
            ret.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            final JSONObject jsonObject = new JSONObject().put(Keys.STATUS_CODE, false);
            renderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
        }
    }

    /**
     * Gets comments of a page specified by the article id for administrator.
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "comments": [{
     *         "oId": "",
     *         "commentName": "",
     *         "commentEmail": "",
     *         "thumbnailUrl": "",
     *         "commentURL": "",
     *         "commentContent": "",
     *         "commentTime": long,
     *         "commentSharpURL": "",
     *         "isReply": boolean
     *      }, ....]
     * }
     * </pre>
     * </p>
     *
     * @param context the specified http request context
     */
    @RequestProcessing(value = "/console/comments/page/{id}", method = HttpMethod.GET)
    public void getPageComments(final RequestContext context) {
        final JsonRenderer renderer = new JsonRenderer();
        context.setRenderer(renderer);
        final JSONObject ret = new JSONObject();
        renderer.setJSONObject(ret);

        try {
            final String pageId = context.pathVar("id");
            final List<JSONObject> comments = commentQueryService.getComments(pageId);

            ret.put(Comment.COMMENTS, comments);
            ret.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);

            final JSONObject jsonObject = new JSONObject().put(Keys.STATUS_CODE, false);
            renderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, langPropsService.get("getFailLabel"));
        }
    }
}
