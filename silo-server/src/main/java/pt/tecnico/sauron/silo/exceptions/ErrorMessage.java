package pt.tecnico.sauron.silo.exceptions;

public enum ErrorMessage {
	INVALID_TARGET_ID("The id %s is not valid."),
	TARGET_NOT_FOUND("The id %s does not exist."),
	INVALID_TARGET_TYPE("The target type %s is not valid."),
	EYE_NOT_FOUND("The eye with name %s is not valid"),
	INVALID_NAME("Invalid name"),
	INVALID_COORDS("Invalid coords"),
	EYE_ALREADY_EXISTS("The eye with name %s already exists."),
	INVALID_TIMESTAMP("Timestamp is Invalid.");
	public final String label;

	ErrorMessage(String label) { this.label = label; }

}
