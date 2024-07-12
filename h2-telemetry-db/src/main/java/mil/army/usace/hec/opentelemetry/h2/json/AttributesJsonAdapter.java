package mil.army.usace.hec.opentelemetry.h2.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AttributesJsonAdapter extends TypeAdapter<Attributes> {
    @Override
    public void write(JsonWriter jsonWriter, Attributes attributes) throws IOException {
        if(attributes == null) {
            jsonWriter.nullValue();
            return;
        }
        Map<AttributeKey<?>, Object> attributesMap = attributes.asMap();
        jsonWriter.beginObject();
        for(Map.Entry<AttributeKey<?>, Object> entry : attributesMap.entrySet()) {
            AttributeKey<?> key = entry.getKey();
            String keyName = key.getKey();
            AttributeType type = key.getType();
            Object value = entry.getValue();

            jsonWriter.name(keyName);
            jsonWriter.beginObject();
            jsonWriter.name("type").value(type.name());
            jsonWriter.name("value");
            if(type == AttributeType.STRING) {
                jsonWriter.value((String) value);
            } else if(type == AttributeType.BOOLEAN) {
                jsonWriter.value((Boolean) value);
            } else if(type == AttributeType.LONG) {
                jsonWriter.value((Long) value);
            } else if(type == AttributeType.DOUBLE) {
                jsonWriter.value((Double) value);
            } else {
                // Probably an array type
                List<?> valAsList = (List<?>)value;
                jsonWriter.beginArray();
                for(Object obj : valAsList) {
                    if(type == AttributeType.BOOLEAN_ARRAY) {
                        jsonWriter.value((Boolean) obj);
                    } else if(type == AttributeType.STRING_ARRAY) {
                        jsonWriter.value((String) obj);
                    } else if(type == AttributeType.LONG_ARRAY) {
                        jsonWriter.value((Long) obj);
                    } else if(type == AttributeType.DOUBLE_ARRAY) {
                        jsonWriter.value((Double) obj);
                    }
                }
                jsonWriter.endArray();
            }
            jsonWriter.endObject();
        }
        jsonWriter.endObject();

    }

    @Override
    public Attributes read(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();

        AttributesBuilder builder = Attributes.builder();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            jsonReader.beginObject();
            String nextName = jsonReader.nextName();
            if(!"type".equals(nextName)) {
                throw new IllegalStateException("Expected 'type' field");
            }
            String type = jsonReader.nextString();
            AttributeType attributeType = AttributeType.valueOf(type);
            nextName = jsonReader.nextName();
            if(!"value".equals(nextName)) {
                throw new IllegalStateException("Expected 'value' field");
            }
            if(attributeType == AttributeType.STRING) {
                builder.put(AttributeKey.stringKey(key), jsonReader.nextString());
            } else if(attributeType == AttributeType.BOOLEAN) {
                builder.put(AttributeKey.booleanKey(key), jsonReader.nextBoolean());
            } else if(attributeType == AttributeType.LONG) {
                builder.put(AttributeKey.longKey(key), jsonReader.nextLong());
            } else if(attributeType == AttributeType.DOUBLE) {
                builder.put(AttributeKey.doubleKey(key), jsonReader.nextDouble());
            } else {
                // Array value
                jsonReader.beginArray();
                if(attributeType == AttributeType.BOOLEAN_ARRAY) {
                    List<Boolean> values = new ArrayList<>();
                    while(jsonReader.hasNext()) {
                        values.add(jsonReader.nextBoolean());
                    }
                    builder.put(AttributeKey.booleanArrayKey(key), values);
                } else if(attributeType == AttributeType.STRING_ARRAY) {
                    List<String> values = new ArrayList<>();
                    while(jsonReader.hasNext()) {
                        values.add(jsonReader.nextString());
                    }
                    builder.put(AttributeKey.stringArrayKey(key), values);
                } else if(attributeType == AttributeType.LONG_ARRAY) {
                    List<Long> values = new ArrayList<>();
                    while(jsonReader.hasNext()) {
                        values.add(jsonReader.nextLong());
                    }
                    builder.put(AttributeKey.longArrayKey(key), values);
                } else if(attributeType == AttributeType.DOUBLE_ARRAY) {
                    List<Double> values = new ArrayList<>();
                    while (jsonReader.hasNext()) {
                        values.add(jsonReader.nextDouble());
                    }
                    builder.put(AttributeKey.doubleArrayKey(key), values);
                }
                jsonReader.endArray();
            }
            jsonReader.endObject();
        }

        jsonReader.endObject();
        return builder.build();
    }
}
