package pt.tecnico.sauron.silo.eyes;

public class Eye {

	private String name;

	private Coords coords;

	public Eye (String name, Coords coords) {
		this.name   = name;
		this.coords = coords;
	}

	public String getName () {
		return name;
	}

	public void setName (String name) {
		this.name = name;
	}

	public Coords getCoords () {
		return coords;
	}

	public void setCoords (Coords coords) {
		this.coords = coords;
	}
}
