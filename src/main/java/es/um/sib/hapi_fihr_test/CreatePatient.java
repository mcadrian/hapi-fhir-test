package es.um.sib.hapi_fihr_test;

import java.io.IOException;

import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;

import ca.uhn.fhir.context.FhirContext;


public class CreatePatient {

	// context - create this once, as it's an expensive operation
	// see http://hapifhir.io/doc_intro.html
	private static final FhirContext ctx = FhirContext.forDstu3();
	
	public static void main(String[] args) throws IOException {
		createPatient();
	}

	private static void createPatient() throws IOException {
		Patient patient = new Patient();
		patient.addName().setUse(HumanName.NameUse.OFFICIAL).addPrefix("Mr")
				.setFamily("Fhirman").addGiven("Sam");
		patient.addIdentifier()
				.setSystem("http://ns.electronichealth.net.au/id/hi/ihi/1.0")
				.setValue("8003608166690503");

		System.out
				.println("Press Enter to serialise Resource to the console as XML.");
		System.in.read();

		// create a new XML parser and serialize our Patient object with it
		String encoded = ctx.newXmlParser().setPrettyPrint(true)
				.encodeResourceToString(patient);

		System.out.println(encoded);

		System.out.println("Press Enter to end.");
		System.in.read();
	}
}
