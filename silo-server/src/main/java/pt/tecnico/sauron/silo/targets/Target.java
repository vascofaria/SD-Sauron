package pt.tecnico.sauron.silo.targets;

import pt.tecnico.sauron.silo.grpc.TargetType;
import pt.tecnico.sauron.silo.grpc.Observation;

import pt.tecnico.sauron.silo.eyes.ObservationRecord;

import java.util.stream.Collectors;

import com.google.protobuf.util.Timestamps;

import java.util.List;
import java.util.ArrayList;

public abstract class Target {
	private List<ObservationRecord> timeline = new ArrayList<>();

	public void addObservation(ObservationRecord observation) { this.timeline.add(observation); }
	public List<ObservationRecord> getTimeLine() { return this.timeline; }
	public ObservationRecord getLastObservation() { return this.timeline.get(this.timeline.size() - 1); }

	public abstract String getId();
	public abstract TargetType getType();
	public abstract boolean matchId(String id);

	public Observation track() {
		ObservationRecord record = this.getLastObservation();
		return Observation.newBuilder()
						.setTargetType(this.getType())
						.setTargetId(this.getId())
						.setDateTime(record.getDateTime())
						.setCamName(record.getEyeName())
						.build();
	}

	public List<Observation> trace() {
		return timeline.stream()
				.map(record -> Observation.newBuilder()
									.setTargetType(this.getType())
									.setTargetId(this.getId())
									.setDateTime(record.getDateTime())
									.setCamName(record.getEyeName())
									.build())
				.sorted((o1, o2) -> Timestamps.between(o1.getDateTime(),o2.getDateTime()).getSeconds() <= 0 ? -1 : 1)
				.collect(Collectors.toList());
	}

}
