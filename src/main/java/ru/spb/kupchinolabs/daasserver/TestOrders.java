package ru.spb.kupchinolabs.daasserver;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class TestOrders {

    static List<JsonObject> orders = new ArrayList<>();

    static {
        orders.add(order("Почтамтская ул.,  д. 9, лит. А", "Измайловский пр., д. 7, лит. А", "пицца", "59.932189", "30.300714"));
        orders.add(order("Измайловский пр., д. 7, лит. А", "Английский пр., д. 45, лит. А", "шаверма", "59.916419", "30.306094"));
        orders.add(order("Английский пр., д. 45, лит. А", "ул. Подольская, д. 1-3-5, лит. А", "шаурма", "59.917199", "30.289916"));
        orders.add(order("ул. Подольская, д. 1-3-5, лит. А", "Рижский пр., д. 50, лит. А", "суши", "59.917389", "30.322354"));
        orders.add(order("Рижский пр., д. 50, лит. А", "Сенная пл., д. 13, лит. А", "лапша вок", "59.912937", "30.279495"));
        orders.add(order("Сенная пл., д. 13, лит. А", "Вознесенский пр., д. 49, лит. А", "гречневая каша", "59.926629", "30.316371"));
        orders.add(order("Вознесенский пр., д. 49, лит. А", "пл. Труда, д. 4, лит. В", "макароны по флотски", "59.92148", "30.307568"));
        orders.add(order("пл. Труда, д. 4, лит. В", "Исаакиевская пл., д. 6", "блины со сметанкой", "59.913893", "30.298405"));
    }

    static JsonObject order(String from, String to, String comment, String latitude, String longitude) {
        final JsonObject office = new JsonObject();
        office.put(Constants.FROM, from);
        office.put(Constants.TO, to);
        office.put(Constants.COMMENT, comment);
        office.put("longitude", longitude);
        office.put("latitude", latitude);
        return office;

    }

}
