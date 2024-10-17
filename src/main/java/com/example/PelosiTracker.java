package com.example;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PelosiTracker {

    // Clave de API para acceder a los datos financieros
    private static final String API_KEY = "3q4RT5NbA5bWB3FeiP51ZDBVVtlRJ3K6";
    private static final String TELEGRAM_BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN");
    private static final String TELEGRAM_CHAT_ID = System.getenv("TELEGRAM_CHAT_ID");

    private static List<Movimiento> movimientosPrevios = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Bienvenido al programa de seguimiento de inversiones de Nancy Pelosi.");
        System.out.println("Este programa te enviará notificaciones de Telegram sobre los movimientos de mercado de Nancy Pelosi.");
        System.out.println("=================================");

        // Enviar un mensaje de prueba si se ejecuta con el argumento "test"
        if (args.length > 0 && args.equals("test")) {
            enviarMensajeDePrueba();
            return; // Salir después de enviar el mensaje de prueba
        }

        // Registrar la ejecución del programa
        registrarEjecucion();

        while (true) {
            try {
                // Obtener movimientos de Nancy Pelosi
                List<Movimiento> movimientos = obtenerMovimientosPelosi();

                // Comparar con los movimientos previos y enviar notificación si hay nuevos movimientos
                for (Movimiento movimiento : movimientos) {
                    if (!movimientosPrevios.contains(movimiento)) {
                        enviarNotificacionTelegram(movimiento);
                        movimientosPrevios.add(movimiento);
                    }
                }

                // Esperar un tiempo antes de volver a verificar (por ejemplo, 1 hora)
                Thread.sleep(3600000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Método para registrar la ejecución del programa
    private static void registrarEjecucion() {
        try (FileWriter writer = new FileWriter("cron_log.txt", true)) {
            writer.write("Programa ejecutado a las: " + new java.util.Date() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para obtener los movimientos de Nancy Pelosi
    public static List<Movimiento> obtenerMovimientosPelosi() throws IOException, ParseException {
        List<Movimiento> movimientos = new ArrayList<>();
        String url = "https://api.quiverquant.com/beta/historical/congresstrading/Nancy%20Pelosi?apikey=" + API_KEY;
        HttpResponse<JsonNode> response = Unirest.get(url).asJson();

        JSONParser parser = new JSONParser();
        JSONArray jsonArray;
        try {
            jsonArray = (JSONArray) parser.parse(response.getBody().toString());
        } catch (ClassCastException e) {
            JSONObject jsonObject = (JSONObject) parser.parse(response.getBody().toString());
            jsonArray = (JSONArray) jsonObject.get("transactions");
        }

        if (jsonArray != null) {
            for (Object obj : jsonArray) {
                JSONObject transaction = (JSONObject) obj;
                String fecha = (String) transaction.get("TransactionDate");
                String ticker = (String) transaction.get("Ticker");
                String tipo = (String) transaction.get("TransactionType");
                long cantidad = (long) transaction.get("Amount");
                movimientos.add(new Movimiento(fecha, ticker, tipo, cantidad));
            }
        } else {
            System.err.println("No se encontraron transacciones.");
        }

        return movimientos;
    }

    // Método para enviar notificación de Telegram con el movimiento
    public static void enviarNotificacionTelegram(Movimiento movimiento) {
        String mensaje = "Nuevo movimiento de Nancy Pelosi:\n\n" +
                "Fecha: " + movimiento.fecha + "\n" +
                "Ticker: " + movimiento.ticker + "\n" +
                "Tipo: " + movimiento.tipo + "\n" +
                "Cantidad: " + movimiento.cantidad;

        HttpResponse<JsonNode> response = Unirest.post("https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage")
                .field("chat_id", TELEGRAM_CHAT_ID)
                .field("text", mensaje)
                .asJson();

        if (response.getStatus() == 200) {
            System.out.println("Notificación de Telegram enviada con éxito.");
        } else {
            System.err.println("Error al enviar la notificación de Telegram.");
        }
    }

    // Método para enviar un mensaje de prueba
    public static void enviarMensajeDePrueba() {
        String mensaje = "Este es un mensaje de prueba para verificar que el bot de Telegram está funcionando correctamente.";

        HttpResponse<JsonNode> response = Unirest.post("https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage")
                .field("chat_id", TELEGRAM_CHAT_ID)
                .field("text", mensaje)
                .asJson();

        if (response.getStatus() == 200) {
            System.out.println("Mensaje de prueba enviado con éxito.");
        } else {
            System.err.println("Error al enviar el mensaje de prueba.");
        }
    }

    // Clase para representar un movimiento de mercado
    public static class Movimiento {
        String fecha;
        String ticker;
        String tipo;
        long cantidad;

        Movimiento(String fecha, String ticker, String tipo, long cantidad) {
            this.fecha = fecha;
            this.ticker = ticker;
            this.tipo = tipo;
            this.cantidad = cantidad;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Movimiento that = (Movimiento) o;

            if (cantidad != that.cantidad) return false;
            if (!fecha.equals(that.fecha)) return false;
            if (!ticker.equals(that.ticker)) return false;
            return tipo.equals(that.tipo);
        }

        @Override
        public int hashCode() {
            int result = fecha.hashCode();
            result = 31 * result + ticker.hashCode();
            result = 31 * result + tipo.hashCode();
            result = 31 * result + (int) (cantidad ^ (cantidad >>> 32));
            return result;
        }
    }
}
