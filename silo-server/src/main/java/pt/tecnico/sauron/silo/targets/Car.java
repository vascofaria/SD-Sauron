package pt.tecnico.sauron.silo.targets;

import pt.tecnico.sauron.silo.grpc.TargetType;

import pt.tecnico.sauron.silo.targets.Target;
import pt.tecnico.sauron.silo.eyes.ObservationRecord;

import pt.tecnico.sauron.silo.exceptions.SiloException;
import pt.tecnico.sauron.silo.exceptions.ErrorMessage;

public class Car extends Target {
	private String id;

	public Car(String id) throws SiloException {
		super();
		this.id = checkId(id);
	}

	public String getId() throws SiloException{ return this.id; }
	public TargetType getType() { return TargetType.CAR; }

	public String checkId(String id) {
		int letter_group=0;
		int num_group=0;
		if(id.length()==6) {
			for(int i=0;i<3;i++) {
				if (id.charAt(i*2) >= 'A' && id.charAt(i*2) <= 'Z') {
					if (!(id.charAt(i*2+1) >= 'A' && id.charAt(i*2 +1) <= 'Z'))
						throw new SiloException(ErrorMessage.INVALID_TARGET_ID,id);
					else
						letter_group++;
				}
				if (id.charAt(i*2) >= '0' && id.charAt(i*2) <= '9') {
					if (!(id.charAt(i*2+1) >= '0' && id.charAt(i*2 +1) <= '9'))
						throw new SiloException(ErrorMessage.INVALID_TARGET_ID,id);
					else
						num_group++;
				}
			}
		}
		else 
			throw new SiloException(ErrorMessage.INVALID_TARGET_ID,id);
		if(num_group>=3 || letter_group>=3)
			throw new SiloException(ErrorMessage.INVALID_TARGET_ID,id);
		return id;
	}


	@Override
	public boolean matchId(String id) throws SiloException {

		char[] chars = id.toCharArray();

		StringBuilder sb = new StringBuilder();
		for (char c : chars) {
			if (c >= '0' && c <= '9') sb.append(c);
			else if (c >= 'A' && c <= 'Z') sb.append(c);
			else if (c == '*') sb.append("(.*)");
			else throw new SiloException(ErrorMessage.INVALID_TARGET_ID, id);
		}
		return this.id.matches(sb.toString());
	}

}
