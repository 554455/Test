package org.example;

import java.io.IOException;
import java.util.concurrent.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

public class CrptApi {
    private final int requestLimit;
    private final long interval;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.interval = timeUnit.toMillis(1);
        this.semaphore = new Semaphore(requestLimit);

        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    public String createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();

        RequestBody requestBody = RequestBody.create(
                objectMapper.writeValueAsString(document),
                MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url("https://ismp.crpt.ru/api/v3/lk/documents/create")
                .post(requestBody)
                .addHeader("Signature", signature)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }
}
