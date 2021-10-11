package pt.tecnico.sauron.silo;

import pt.tecnico.sauron.silo.grpc.TargetType;
import pt.tecnico.sauron.silo.grpc.Observation;

import pt.tecnico.sauron.silo.targets.Car;
import pt.tecnico.sauron.silo.targets.Person;

import pt.tecnico.sauron.silo.eyes.Eye;
import pt.tecnico.sauron.silo.eyes.Coords;
import pt.tecnico.sauron.silo.eyes.ObservationRecord;

import pt.tecnico.sauron.silo.exceptions.SiloException;
import pt.tecnico.sauron.silo.exceptions.ErrorMessage;

import com.google.protobuf.util.Timestamps;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Duration;

import java.util.stream.Collectors;
import java.util.List;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Silo {

	private Map<String, Eye> eyes; /* Key: cam_name*/
	private Map<String, Car> carsObservations; /* Key: observation_id */
	private Map<Long, Person> peopleObservations; /* Key: observation_id */

	public Silo() {
		this.eyes = new ConcurrentHashMap<>();
		this.carsObservations = new ConcurrentHashMap<>();
		this.peopleObservations = new ConcurrentHashMap<>();
	}

	public void ctrlClear() {
		carsObservations.clear();
		peopleObservations.clear();
		eyes.clear();
	}

	public boolean camJoin (String camName, Double latitude, Double longitude) throws SiloException {

		Eye eye;

		// check the size of the cam name
		if (camName.length() < 3 || camName.length() > 15) {
			throw new SiloException(ErrorMessage.INVALID_NAME);
		}
		
		// check if name already exists
		if (eyes.containsKey(camName)) {
			Eye dup = eyes.get(camName);
			// check if the cam is the same
			if (!dup.getCoords().getLatitude().equals(latitude) || !dup.getCoords().getLongitude().equals(longitude))
				throw new SiloException(ErrorMessage.EYE_ALREADY_EXISTS, camName);
			else
				return false;
		}
		// check the latitude and longitude range
		else if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
			throw new SiloException(ErrorMessage.INVALID_COORDS, "");
		}
		else {
			// create a new camera
			eye = new Eye(camName, new Coords(latitude, longitude));
			eyes.put(camName, eye);
		}
		return true;
	}

	public Coords camInfo (String camName) throws SiloException {

		// check if camera exists
		if (eyes.containsKey(camName)) {
			return eyes.get(camName).getCoords();
		}
		else {
			throw new SiloException(ErrorMessage.EYE_NOT_FOUND, camName);
		}
	}

	public List<Observation> report(List<Observation> observationsList) throws SiloException {
		List<Observation> acceptedObservations = new ArrayList<>();
		String camName = observationsList.get(0).getCamName();

		// check if camera exists
		if(!(eyes.containsKey(camName)))
			throw new SiloException(ErrorMessage.EYE_NOT_FOUND, camName);
			
		// for each given observation
		observationsList.stream()
			.forEach(observation -> {

				// check if its valid Datetime (> current)
				Instant time = Instant.now();
				Timestamp timestampNow = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
				    .setNanos(time.getNano()).build();
				
				Timestamp timestamp = observation.getDateTime();
				if(timestamp.getSeconds() > timestampNow.getSeconds())
					throw new SiloException(ErrorMessage.INVALID_TIMESTAMP);
				
				ObservationRecord observationRecord = new ObservationRecord(camName,timestamp);
				if(observation.getTargetId()==null || observation.getTargetId().isBlank() || observation.getTargetId().isEmpty())
					throw new SiloException(ErrorMessage.INVALID_TARGET_ID,observation.getTargetId() );
	
				switch(observation.getTargetType()) {
				
				case CAR :
					Car car = carsObservations.get(observation.getTargetId());
					// if the car was never reported before create a new one
					if (car == null) {
						car = new Car(observation.getTargetId());
						carsObservations.put(observation.getTargetId(), car);
					}
					car.addObservation(observationRecord);
					acceptedObservations.add(observation);
					break;
					
					
				case PERSON :
					try {
						Long id = Long.parseLong(observation.getTargetId());
						Person person = peopleObservations.get(Long.parseLong(observation.getTargetId()));
						// if the person was never reported before create a new one
						if (person == null) {
							if(id <= 0)
								throw new SiloException(ErrorMessage.INVALID_TARGET_ID,observation.getTargetId() );
							person = new Person(id);
							peopleObservations.put(id, person);
	
						}
						person.addObservation(observationRecord);
						acceptedObservations.add(observation);

					} catch(NumberFormatException e) {
						throw new SiloException(ErrorMessage.INVALID_TARGET_ID,observation.getTargetId() );
					}
					break;
				default:
					
					throw new SiloException(ErrorMessage.INVALID_TARGET_TYPE, observation.getTargetId());
				}
			}
		);
		
		return acceptedObservations;
	}

	public Observation track(TargetType targetType, String targetId) throws SiloException {

		switch (targetType) {

			case CAR:
				Car car = carsObservations.get(targetId);
				// check if car exists
				if (car == null)
					throw new SiloException(ErrorMessage.TARGET_NOT_FOUND, targetId);
				return car.track();

			case PERSON:
				try {
					
					Long tID = Long.parseLong(targetId);
					Person person = peopleObservations.get(tID);
					// check if person exists
					if (person == null)
						throw new SiloException(ErrorMessage.TARGET_NOT_FOUND, targetId);
					
					return person.track();

				} catch(NumberFormatException e) {
					throw new SiloException(ErrorMessage.INVALID_TARGET_ID, targetId);
				}

			default:
				throw new SiloException(ErrorMessage.INVALID_TARGET_TYPE, targetType.toString());
		}
	}

	public List<Observation> trackMatch(TargetType targetType, String targetId) throws SiloException {

		switch (targetType) {

			case CAR:
				// return the matched cars sorted by datetime timestamp
				return carsObservations.entrySet().stream()
						.filter(entry -> entry.getValue().matchId(targetId))
						.map(entry -> entry.getValue().track())
						.sorted((o1, o2) -> Timestamps.between(o1.getDateTime(),o2.getDateTime()).getSeconds() <= 0 ? -1 : 1)
						.collect(Collectors.toList());

			case PERSON:
				// return the matched people sorted by datetime timestamp
				return peopleObservations.entrySet().stream()
						.filter(entry -> entry.getValue().matchId(targetId))
						.map(entry -> entry.getValue().track())
						.sorted((o1, o2) -> Timestamps.between(o1.getDateTime(),o2.getDateTime()).getSeconds() <= 0 ? -1 : 1)
						.collect(Collectors.toList());

			default:
				throw new SiloException(ErrorMessage.INVALID_TARGET_TYPE, targetType.toString());
		}
	}

	public List<Observation> trace(TargetType targetType, String targetId) throws SiloException {

		switch (targetType) {

			case CAR:
				Car car = carsObservations.get(targetId);
				// check if car exists
				if (car == null)
					throw new SiloException(ErrorMessage.TARGET_NOT_FOUND, targetId);
				return car.trace();

			case PERSON:
				try {

					Long tID = Long.parseLong(targetId);
					Person person = peopleObservations.get(tID);
					// check if person exists
					if (person == null)
						throw new SiloException(ErrorMessage.TARGET_NOT_FOUND, targetId);

					return person.trace();
				
				} catch(NumberFormatException e) {
					throw new SiloException(ErrorMessage.INVALID_TARGET_ID, targetId);
				}

			default:
				throw new SiloException(ErrorMessage.INVALID_TARGET_TYPE, targetType.toString());
		}
	}

}
