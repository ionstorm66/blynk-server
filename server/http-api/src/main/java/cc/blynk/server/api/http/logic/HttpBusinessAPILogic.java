package cc.blynk.server.api.http.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.api.http.pojo.business.BusinessProject;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.handlers.http.rest.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static cc.blynk.server.handlers.http.rest.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.12.15.
 */
@Path("/")
@SuppressWarnings("unchecked")
public class HttpBusinessAPILogic {

    private static final Logger log = LogManager.getLogger(HttpBusinessAPILogic.class);

    private final UserDao userDao;

    public HttpBusinessAPILogic(Holder holder) {
        this(holder.userDao);
    }

    private HttpBusinessAPILogic(UserDao userDao) {
        this.userDao = userDao;
    }

    @GET
    @Path("{token}/query")
    public Response getDashboard(@PathParam("token") String token,
                                 @QueryParam("name") String name,
                                 @QueryParam("groupBy") List<String> groupByList,
                                 @QueryParam("aggregation") String aggregation,
                                 @QueryParam("pin") String pin,
                                 @QueryParam("value") String value) {

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        List<DashBoard> projects = new ArrayList<>(Arrays.asList(user.profile.dashBoards));

        projects = filterByProjectName(projects, name);
        projects = filterByValue(projects, pin, value);

        if (groupByList == null || aggregation == null) {
            return ok(transform(projects));
        }

        Map<Map, Long> groupingResult = groupBy(projects, groupByList, aggregation);

        return ok(transform(groupingResult, aggregation));
    }

    private static List<Map> transform(Map<Map, Long> groupingResult, String aggregation) {
        List<Map> result = new ArrayList<>(groupingResult.size());

        for (Map.Entry<Map, Long> entry : groupingResult.entrySet()) {
            Map<String, Object> key = entry.getKey();
            key.put(aggregation, entry.getValue());
            result.add(key);
        }

        return result;
    }

    /**
     * Simplifies output object.
     */
    private static List<BusinessProject> transform(List<DashBoard> projects) {
        return projects.stream().map(BusinessProject::new).collect(Collectors.toList());
    }

    private static Map<Map, Long> groupBy(List<DashBoard> projects, List<String> groupByList, String aggregation) {
        return projects.stream().collect(Collectors.groupingBy(
                proj -> {
                    Map<String, Object> result = new HashMap<>();
                    for (String groupBy : groupByList) {
                        switch (groupBy) {
                            case "name" :
                                result.put(groupBy, proj.name);
                                break;
                            default:
                                result.put(groupBy, proj.metadata.get(groupBy));
                                break;
                        }
                    }

                    return result;
                },
                getAggregation(aggregation)
               )
        );
    }

    private static <T> Collector<T, ?, Long> getAggregation(String aggregation) {
        switch (aggregation) {
            case "count" :
                return Collectors.counting();
            default:
                throw new RuntimeException("Not supported aggregation function " + aggregation);
        }
    }

    private static List<DashBoard> filterByValue(List<DashBoard> projects, String pin, String value) {
        if (value == null) {
            return projects;
        }

        if (pin != null) {
            return filterByValueAndPin(projects, pin, value);
        }

        return filterByValue(projects, value);
    }

    private static List<DashBoard> filterByValueAndPin(List<DashBoard> projects, String pin, String value) {
        PinType pinType = PinType.getPinType(pin.charAt(0));
        byte pinIndex = Byte.parseByte(pin.substring(1));

        return projects.stream().filter(
                project -> {
                    Widget widget = project.findWidgetByPin(pinIndex, pinType);
                    if (widget == null) {
                        return false;
                    }
                    String widgetValue = widget.getValue(pinIndex, pinType);
                    return value.equalsIgnoreCase(widgetValue);
                }
        ).collect(Collectors.toList());
    }

    private static List<DashBoard> filterByValue(List<DashBoard> projects, String value) {
        return projects.stream().filter(
                project -> {
                    for (Widget widget : project.widgets) {
                        if (widget.hasValue(value)) {
                            return true;
                        }
                    }
                    return false;
                }
        ).collect(Collectors.toList());
    }

    private static List<DashBoard> filterByProjectName(List<DashBoard> projects, String name) {
        if (name == null) {
            return projects;
        }
        return projects.stream().filter(
                project -> name.equalsIgnoreCase(project.name)
        ).collect(Collectors.toList());
    }

}
