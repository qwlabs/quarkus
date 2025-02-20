package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TracerRouter;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class VertxOpenTelemetryForwardedTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TestSpanExporter.class)
                    .addClass(TracerRouter.class));

    @Inject
    TestSpanExporter testSpanExporter;

    @Test
    void trace() {
        RestAssured.given().header("Forwarded", "for=192.0.2.60;proto=http;by=203.0.113.43")
                .when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        List<SpanData> spans = testSpanExporter.getFinishedSpanItems(2);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("192.0.2.60", server.getAttributes().get(HTTP_CLIENT_IP));
    }
}
