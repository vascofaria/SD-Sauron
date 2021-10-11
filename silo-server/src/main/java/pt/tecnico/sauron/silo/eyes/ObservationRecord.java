package pt.tecnico.sauron.silo.eyes;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;


public class ObservationRecord {
	private String eyeName;
	private Timestamp dateTime;

	public ObservationRecord(String eyeName, Timestamp dateTime) {
		this.eyeName = eyeName;
		this.dateTime = dateTime;
	}

	public String getEyeName() { return this.eyeName; }
	public Timestamp getDateTime() { return this.dateTime; }
	public String getDateTimeString() {
		return this.dateTime.toString();
	}

}
