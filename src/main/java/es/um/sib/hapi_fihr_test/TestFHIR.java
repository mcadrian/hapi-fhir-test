package es.um.sib.hapi_fihr_test;

import java.io.IOException;
import java.util.Scanner;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

//import ca.uhn.fhir.rest.client.api.IGenericClient;

public class TestFHIR {

	// context - create this once, as it's an expensive operation
	// see http://hapifhir.io/doc_intro.html
	private static final FhirContext ctx = FhirContext.forDstu3();
	private static final String serverBaseUrl = "http://sqlonfhir-stu3.azurewebsites.net/fhir";

	// create the RESTful client to work with our FHIR server
	// see http://hapifhir.io/doc_rest_client.html
	private static final IGenericClient client = ctx
			.newRestfulGenericClient(serverBaseUrl);
	private static Patient patient;

	public static void main(String[] args) throws IOException {
		Scanner scanner = null;
		String input = "";

		while (!input.toLowerCase().equals("quit")) {
			System.out.println("Type an option:");
			System.out.println("\t - Print patient as XML (type xml)");
			System.out.println("\t - Print patient as JSON (type json)");
			System.out.println("\t - Upload");
			System.out.println("\t - Delete");
			System.out.println("\t - Search");
			System.out.println("\t - Execute");
			System.out.println("\t - Quit");
			scanner = new Scanner(System.in);
			input = scanner.nextLine();

			switch (input.toLowerCase()) {
			case "xml":
				printPatientAsXML();
				break;
			case "json":
				printPatientAsJSON();
				break;
			case "upload":
				uploadPatient();
				break;
			case "delete":
				deletePatient();
				break;
			case "search":
				searchPatient();
				break;
			case "execute":
				executeOperation();
				break;
			case "quit":
				break;
			default:
				System.out.println("There is no option \"" + input
						+ "\". Please try again.");
				break;
			}

			System.out.println("Press Enter for next action.");
			System.in.read();
		}

		scanner.close();
	}

	private static Patient createPatient() throws IOException {
		if (patient == null) {
			patient = new Patient();
			patient.addName().setUse(HumanName.NameUse.OFFICIAL)
					.addPrefix("Mr").setFamily("Fantastical").addGiven("Sam");
			patient.addIdentifier()
					.setSystem(
							"http://ns.electronichealth.net.au/id/hi/ihi/1.0")
					.setValue("1234567890123456");
		}
		return patient;
	}

	private static void printPatientAsXML() throws IOException {
		createPatient();

		System.out
				.println("Press Enter to serialise Resource to the console as XML.");
		System.in.read();

		// create a new XML parser and serialize our Patient object with it
		String encoded = ctx.newXmlParser().setPrettyPrint(true)
				.encodeResourceToString(patient);

		System.out.println(encoded);

	}

	private static void printPatientAsJSON() throws IOException {
		createPatient();

		System.out
				.println("Press Enter to serialise Resource to the console as JSON.");
		System.in.read();

		// create a new XML parser and serialize our Patient object with it
		String encoded = ctx.newJsonParser().setPrettyPrint(true)
				.encodeResourceToString(patient);

		System.out.println(encoded);

	}

	private static void uploadPatient() throws IOException {
		createPatient();

		System.out.println("Press Enter to send to server: " + serverBaseUrl);
		System.in.read();

		try {
			// send our resource up - result will be stored in 'outcome'
			// see http://hapifhir.io/doc_rest_client.html#Create_-_Type
			MethodOutcome outcome = client.create().resource(patient)
					.prettyPrint().encodedXml().execute();

			IdType id = (IdType) outcome.getId();
			System.out.println("Resource is available at: " + id.getValue());

			IParser xmlParser = ctx.newXmlParser().setPrettyPrint(true);
			patient = (Patient) outcome.getResource();
			System.out.println("This is what we sent up: \n"
					+ xmlParser.encodeResourceToString(patient)
					+ "\n\nThis is what we received: \n"
					+ xmlParser.encodeResourceToString(patient));
		} catch (DataFormatException e) {
			System.out.println("An error occurred trying to upload:");
			e.printStackTrace();
		}
	}

	private static void increaseTimeouts() {
		// increase timeouts since the server might be powered down
		// see http://hapifhir.io/doc_rest_client_http_config.html
		ctx.getRestfulClientFactory().setConnectTimeout(60 * 1000);
		ctx.getRestfulClientFactory().setSocketTimeout(60 * 1000);
	}

	private static void searchPatient() throws IOException {
		createPatient();
		increaseTimeouts();

		System.out.println("Press Enter to send to server: " + serverBaseUrl);
		System.in.read();

		try {
			// perform a search for Patients based on the family name
			// see http://hapifhir.io/doc_rest_client.html#SearchQuery_-_Type
			Bundle response = client
					.search()
					.forResource(Patient.class)
					.where(Patient.FAMILY.matches().values(
							patient.getName().get(0).getFamily()))
					.returnBundle(Bundle.class).execute();

			System.out.println("Found " + response.getTotal() + " "
					+ patient.getName().get(0).getFamily()
					+ ". Their logical IDs are:");
			patient = (Patient) response.getEntry().get(0).getResource();
			response.getEntry().forEach((entry) -> {
				// within each entry is a resource - print its logical ID
					System.out.println(entry.getResource().getIdElement()
							.getIdPart());
				});
		} catch (Exception e) {
			System.out.println("An error occurred trying to search:");
			e.printStackTrace();
		}

	}

	private static void executeOperation() throws IOException {
		createPatient();
		increaseTimeouts();

		client.registerInterceptor(new LoggingInterceptor(true));

		System.out.println("Press Enter to send to server: " + serverBaseUrl);
		System.in.read();

		try {
			// Invoke $everything on our Patient
			// See http://hapifhir.io/doc_rest_client.html#Extended_Operations
			// No input parameters
			Parameters outParams = client.operation()
					.onInstance(new IdType("Patient", patient.getIdElement().getIdPart()))
					.named("$everything").withNoParameters(Parameters.class)
					.execute();

			// FHIR normally returns a 'Parameters' resource to an operation,
			// but in case of a single resource response, it just returns the
			// resource itself. This is why it seems that we have to fish a
			// Bundle out of the resulting Params result - HAPI needs to update
			// for the FHIR shortcut
			Bundle result = (Bundle) outParams.getParameterFirstRep()
					.getResource();

			System.out.println("Received " + result.getTotal()
					+ " results. The resources are:");
			result.getEntry().forEach(
					(entry) -> {
						Resource resource = entry.getResource();
						System.out.println(resource.getResourceType() + "/"
								+ resource.getIdElement().getIdPart());
					});
		} catch (Exception e) {
			System.out
					.println("An error occurred trying to run the operation:");
			e.printStackTrace();
		}
	}

	private static void deletePatient() throws IOException {
		createPatient();

		System.out.println("Press Enter to send to server: " + serverBaseUrl);
		System.in.read();

		try {
			// send our resource up - result will be stored in 'outcome'
			// see http://hapifhir.io/doc_rest_client.html#Create_-_Type

			client.delete()
					.resourceById(patient.getResourceType().name(),
							patient.getIdElement().getIdPart()).execute();

			System.out.println("Resouce deleted");

		} catch (DataFormatException e) {
			System.out.println("An error occurred trying to upload:");
			e.printStackTrace();
		}

	}

}
