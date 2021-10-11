package pt.tecnico.sauron.silo.eyes;

public class Coords {

	private Double latitude;

	private Double longitude;

	public Coords (Double latitude, Double longitude) {
		this.latitude  = latitude;
		this.longitude = longitude;
	}

	public Double getLatitude () {
		return latitude;
	}

	public void setLatitude (Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude () {
		return longitude;
	}

	public void setLongitude () {
		this.longitude = longitude;
	}
}