package mil.army.usace.hec.opentelemetry.h2.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;

import java.io.IOException;
import java.util.Map;

public class TraceStateJsonAdapter extends TypeAdapter<TraceState> {

    @Override
    public void write(JsonWriter out, TraceState value) throws IOException {
        out.beginObject();
        for(Map.Entry<String, String>  entry : value.asMap().entrySet()) {
            out.name(entry.getKey());
            out.value(entry.getValue());
        }
        out.endObject();
    }

    @Override
    public TraceState read(JsonReader in) throws IOException {
        TraceStateBuilder builder = TraceState.builder();
        in.beginObject();
        while(in.hasNext()) {
            builder.put(in.nextName(), in.nextString());
        }
        in.endObject();
        return builder.build();
    }
}
