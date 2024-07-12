package mil.army.usace.hec.opentelemetry.objects;

public abstract class ImmutableID<T extends ID<T>> implements ID<T> {

    private String _id;

    protected ImmutableID(String id) {
        _id = id;
    }

    @Override
    public String getId() {
        return _id;
    }
}
