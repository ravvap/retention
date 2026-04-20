package gov.fdic.tip.governance.exception;
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " with id " + id + " could not be found.");
    }
}
