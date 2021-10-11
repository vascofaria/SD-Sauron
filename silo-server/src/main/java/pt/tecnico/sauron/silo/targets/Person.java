package pt.tecnico.sauron.silo.targets;

import pt.tecnico.sauron.silo.grpc.TargetType;

import pt.tecnico.sauron.silo.targets.Target;
import pt.tecnico.sauron.silo.eyes.ObservationRecord;

import java.lang.StringBuilder;

import pt.tecnico.sauron.silo.exceptions.SiloException;
import pt.tecnico.sauron.silo.exceptions.ErrorMessage;

public class Person extends Target {
	private Long id;

	public Person(Long id) {
		super();
		this.id = id;
	}

	public String getId() { return this.id.toString(); }
	public TargetType getType() { return TargetType.PERSON; }

	@Override
	public boolean matchId(String id) throws SiloException {

		char[] chars = id.toCharArray();

		StringBuilder sb = new StringBuilder();
		for (char c : chars) {
			if (c >= '0' && c <= '9') sb.append(c);
			else if (c == '*') sb.append("(.*)");
			else throw new SiloException(ErrorMessage.INVALID_TARGET_ID, id);
		}
		return this.id.toString().matches(sb.toString());
	}

}
