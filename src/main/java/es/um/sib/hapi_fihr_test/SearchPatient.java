package es.um.sib.hapi_fihr_test;

import java.io.IOException;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class SearchPatient {

	// context - create this once, as it's an expensive operation
	// see http://hapifhir.io/doc_intro.html
	private static final FhirContext ctx = FhirContext.forDstu3();
	private static final String serverBaseUrl = "http://sqlonfhir-stu3.azurewebsites.net/fhir";

	public static void main(String[] args) throws IOException {
		uploadPatient();
	}

	private static void uploadPatient() throws IOException {
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
            // perform a search for Patients with last name 'Fhirman'
            // see http://hapifhir.io/doc_rest_client.html#SearchQuery_-_Type
            Bundle response = client.search()
                    .forResource(Patient.class)
                    .where(Patient.FAMILY.matches().values("Fhirman"))
                    .returnBundle(Bundle.class)
                    .execute();

            System.out.println("Found " + response.getTotal()
                    + " Fhirman patients. Their logical IDs are:");
            response.getEntry().forEach((entry) -> {
                // within each entry is a resource - print its logical ID
                System.out.println(entry.getResource().getIdElement().getIdPart());
            });
        } catch (Exception e) {
            System.out.println("An error occurred trying to search:");
            e.printStackTrace();
        }

        System.out.println("Press Enter to end.");
        System.in.read();
	}

}
