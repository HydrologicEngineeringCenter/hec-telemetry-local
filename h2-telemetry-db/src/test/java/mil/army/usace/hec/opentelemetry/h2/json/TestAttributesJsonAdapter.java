package mil.army.usace.hec.opentelemetry.h2.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAttributesJsonAdapter {

    @Test
    public void testAdapter() throws IOException {
        AttributesJsonAdapter attributesJsonAdapter = new AttributesJsonAdapter();
        var key = AttributeKey.booleanKey("key1");
        var value = Boolean.valueOf(false);
        var key2 = AttributeKey.stringArrayKey("key2");
        var value2 = List.of("value1", "value2");
        Attributes attributes = Attributes.of(key, value, key2, value2);
        StringWriter strWriter = new StringWriter();
        try (JsonWriter writer = new JsonWriter(strWriter)) {
            writer.beginObject();
            writer.name("test");
            attributesJsonAdapter.write(writer, attributes);
            writer.endObject();
        }
        String asJson = strWriter.toString();

        Attributes attr;
        try(StringReader reader = new StringReader(asJson)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.beginObject();
            jsonReader.nextName();
            attr = attributesJsonAdapter.read(jsonReader);
            jsonReader.close();
        }
        assertEquals(attributes, attr);
    }

}
