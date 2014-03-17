package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic Observation exception.
 * @author travis
 *
 */
public class ObservationException extends Exception {

	private static final long serialVersionUID = 1L;

	public ObservationException() {
		super();
	}

	public ObservationException(String message) {
		super(message);
	}

	public ObservationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObservationException(Throwable cause) {
		super(cause);
	}

}
