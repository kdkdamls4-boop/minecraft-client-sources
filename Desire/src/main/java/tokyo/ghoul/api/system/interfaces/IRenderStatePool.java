package tokyo.ghoul.api.system.interfaces;

public interface IRenderStatePool {
    Object desire$pooledState(Object owner);

    void desire$poolState(Object owner, Object state);
}
