package mil.army.usace.hec.opentelemetry.objects;

public interface ID<T extends ID<T>> {

    String getId();

}
