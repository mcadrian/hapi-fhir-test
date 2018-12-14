package es.um.sib.hapi_fihr_test;

import java.io.IOException;

import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class UpdatePatient {

	// context - create this once, as it's an expensive operation
	// see http://hapifhir.io/doc_intro.html
	private static final FhirContext ctx = FhirContext.forDstu3();
	private static final String serverBaseUrl = "http://sqlonfhir-stu3.azurewebsites.net/fhir";

	public static void main(String[] args) throws IOException {
		uploadPatient();
	}

	private static void uploadPatient() throws IOException {
		Patient ourPatient = new Patient();

		// you can use the Fluent API to chain calls
		// see http://hapifhir.io/doc_fhirobjects.html
		ourPatient.addName().setUse(HumanName.NameUse.OFFICIAL).addPrefix("Mr")
				.setFamily("Fhirman").addGiven("Sam");
		ourPatient.addIdentifier()
				.setSystem("http://ns.electronichealth.net.au/id/hi/ihi/1.0")
				.setValue("8003608166690503");

		// increase timeouts since the server might be powered down
		// see http://hapifhir.io/doc_rest_client_http_config.html
		ctx.getRestfulClientFactory().setConnectTimeout(60 * 1000);
		ctx.getRestfulClientFactory().setSocketTimeout(60 * 1000);

		// create the RESTful client to work with our FHIR server
		// see http://hapifhir.io/doc_rest_client.html
		IGenericClient client = ctx.newRestfulGenericClient(serverBaseUrl);

		System.out.println("Press Enter to send to server: " + serverBaseUrl);
		System.in.read();

		try {
			// send our resource up - result will be stored in 'outcome'
			// see http://hapifhir.io/doc_rest_client.html#Create_-_Type
			MethodOutcome outcome = client.create().resource(ourPatient)
					.prettyPrint().encodedXml().execute();

			IdType id = (IdType) outcome.getId();
			System.out.println("Resource is available at: " + id.getValue());

			IParser xmlParser = ctx.newXmlParser().setPrettyPrint(true);
			Patient receivedPatient = (Patient) outcome.getResource();
			System.out.println("This is what we sent up: \n"
					+ xmlParser.encodeResourceToString(ourPatient)
					+ "\n\nThis is what we received: \n"
					+ xmlParser.encodeResourceToString(receivedPatient));
		} catch (DataFormatException e) {
			System.out.println("An error occurred trying to upload:");
			e.printStackTrace();
		}

		System.out.println("Press Enter to end.");
		System.in.read();
	}

}
