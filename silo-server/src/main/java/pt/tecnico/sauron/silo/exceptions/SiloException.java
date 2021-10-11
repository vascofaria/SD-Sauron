package pt.tecnico.sauron.silo.exceptions;

import pt.tecnico.sauron.silo.exceptions.ErrorMessage;

public class SiloException extends RuntimeException {
	private final ErrorMessage errorMessage;

	public SiloException(ErrorMessage errorMessage) {
		super(errorMessage.label);
		this.errorMessage = errorMessage;
	}

	public SiloException(ErrorMessage errorMessage, String value) {
		super(String.format(errorMessage.label, value));
		this.errorMessage = errorMessage;
	}

	public ErrorMessage getErrorMessage() {
		return this.errorMessage;
	}
}