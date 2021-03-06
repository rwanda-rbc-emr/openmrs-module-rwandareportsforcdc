/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.rwandasphstudyreports.api;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.report.Report;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.reports.BaseSPHReportConfig;
import org.openmrs.module.rwandasphstudyreports.reports.PatientsWithNoVLAfter8Months;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Tests {@link CDCReportsService}.
 */
public class CDCReportsServiceTest extends BaseModuleContextSensitiveTest {

	CDCReportsService service;

	ReportDefinitionService reportDefinitionService;

	GlobalPropertiesManagement gp;

	PatientService patientService;

	ObsService obsService;

	Concept cd4CountConcept;

	Concept viralLoadConcept;

	EncounterType adultFollowUpEncounterType;

	Concept hivStatusConcept;

	Concept reasonForExitingCareConcept;

	Concept transferOutConcept;

	Concept hivPositive;

	Program hivProgram;

	ProgramWorkflowService programService;

	UserService userService;

	@Before
	public void setup() {
		try {
			executeDataSet("RwandaSPHStudyReportsDataset.xml");

			gp = new GlobalPropertiesManagement();
			service = Context.getService(CDCReportsService.class);
			reportDefinitionService = Context.getService(ReportDefinitionService.class);
			patientService = Context.getPatientService();
			obsService = Context.getObsService();
			programService = Context.getProgramWorkflowService();
			userService = Context.getUserService();
			hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
			cd4CountConcept = gp.getConcept(GlobalPropertyConstants.CD4_COUNT_CONCEPTID);
			viralLoadConcept = gp.getConcept(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID);
			adultFollowUpEncounterType = gp.getEncounterType(GlobalPropertyConstants.ADULT_FOLLOWUP_ENCOUNTER_TYPEID);
			hivStatusConcept = gp.getConcept(GlobalPropertyConstants.HIV_STATUS_CONCEPTID);
			reasonForExitingCareConcept = gp.getConcept(GlobalPropertiesManagement.REASON_FOR_EXITING_CARE);
			transferOutConcept = gp.getConcept(GlobalPropertiesManagement.TRASNFERED_OUT);
			hivPositive = gp.getConcept(GlobalPropertyConstants.HIV_POSITIVE_CONCEPTID);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Ignore
	public void patientsWithNoVLAfter8Months_Report_Test() {
		/*
		 * adultPatientsCohort, hivPositive, noVL8MonthsAfterEnrollmentIntoHIV
		 */
		PatientsWithNoVLAfter8Months report1 = new PatientsWithNoVLAfter8Months();
		try {
			report1.setup();
			
			ReportDefinition rep1 = reportDefinitionService
					.getDefinitionByUuid(BaseSPHReportConfig.PATIENTSWITHNOVLAFTER8MONTHS);
			Report r11 = service.runReport(rep1, null, new Date(), null);
			Patient p432 = patientService.getPatient(432);
			Calendar age30 = Calendar.getInstance();
			Calendar hivEnrollment = Calendar.getInstance();
			Calendar hivPos = Calendar.getInstance();
			Calendar eightMonthsAfterHivEnrollment = Calendar.getInstance();
			DataSetRow row11 = r11.getReportData().getDataSets().get("PatientsWithNoVLAfter8Months").iterator().next();
			Collection<Integer> hivPatients = programService.patientsInProgram(hivProgram, null, null);

			Assert.assertNotNull(row11);

			for (Integer ip : hivPatients) {
				for (PatientProgram pp : programService.getPatientPrograms(patientService.getPatient(ip)))
					programService.purgePatientProgram(pp);
			}

			Report r12 = service.runReport(rep1, null, new Date(), null);
			DataSetRow row12 = r12.getReportData().getDataSets().get("PatientsWithNoVLAfter8Months").iterator().next();

			Assert.assertNotNull(rep1);
			Assert.assertNull(row12);

			hivPos.add(Calendar.MONTH, -12);
			hivEnrollment.add(Calendar.MONTH, -10);
			eightMonthsAfterHivEnrollment.setTime(hivEnrollment.getTime());
			eightMonthsAfterHivEnrollment.add(Calendar.MONTH, 8);
			age30.add(Calendar.YEAR, -30);
			p432.setBirthdate(age30.getTime());

			patientService.savePatient(p432);
			patientService.unvoidPatient(p432);

			Assert.assertNotNull(Context.getService(ReportDefinitionService.class));
			Assert.assertNotNull(p432);

			for (Obs o : obsService.getObservationsByPerson(p432))
				obsService.purgeObs(o);

			Obs hivPositiveObs = service.createObs(hivStatusConcept, hivPositive, hivPos.getTime(), null);

			obsService.saveObs(hivPositiveObs, null);
			service.enrollPatientInProgram(p432, hivProgram, hivEnrollment.getTime(), null);

			Report r13 = service.runReport(rep1, null, new Date(), null);
			DataSetRow row13 = r13.getReportData().getDataSets().get("PatientsWithNoVLAfter8Months").iterator().next();

			Assert.assertNotNull(row13);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test_checkIfPatientHasNoObsInLastNMonthsAfterProgramInit() {
		Patient patient = patientService.getPatient(10432);

		Assert.assertTrue(service.checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(viralLoadConcept, 8, hivProgram, patient));

		Calendar age30 = Calendar.getInstance();
		Calendar hivEnrollment = Calendar.getInstance();
		Calendar hivPos = Calendar.getInstance();
		Calendar eightMonthsAfterHivEnrollment = Calendar.getInstance();
		Collection<Integer> hivPatients = programService.patientsInProgram(hivProgram, null, null);
		List<Obs> ops = obsService.getObservationsByPerson(patient);
		
		for (Integer ip : hivPatients) {
			for (PatientProgram pp : programService.getPatientPrograms(patientService.getPatient(ip)))
				programService.purgePatientProgram(pp);
		}

		hivPos.add(Calendar.MONTH, -12);
		hivEnrollment.add(Calendar.MONTH, -10);
		eightMonthsAfterHivEnrollment.setTime(hivEnrollment.getTime());
		eightMonthsAfterHivEnrollment.add(Calendar.MONTH, 8);
		age30.add(Calendar.YEAR, -30);
		patient.setBirthdate(age30.getTime());
		for (Obs o : ops)
			obsService.purgeObs(o);

		Obs hivPositiveObs = service.createObs(hivStatusConcept, hivPositive, hivPos.getTime(), null);
		Obs vlObs = service.createObs(viralLoadConcept, 900.0, eightMonthsAfterHivEnrollment.getTime(), null);

		hivPositiveObs.setCreator(userService.getUser(1));
		hivPositiveObs.setPerson(patient);
		vlObs.setPerson(patient);
		vlObs.setCreator(userService.getUser(1));
		obsService.saveObs(hivPositiveObs, null);
		obsService.saveObs(vlObs, null);
		service.enrollPatientInProgram(patient, hivProgram, hivEnrollment.getTime(), null);

		patientService.unvoidPatient(patient);
		patientService.savePatient(patient);

		Assert.assertFalse(service.checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(viralLoadConcept, 8, hivProgram, patient));
		
		List<Obs> os = Context.getObsService().getVoidedObservations();
		
		for (Obs o : os) {
			System.out.println(o);
		}
	}

	@Test
	public void testDatesMatch() {
		Calendar c = Calendar.getInstance();
		Calendar t = (Calendar) c.clone();
		Calendar h = (Calendar) c.clone();
		Calendar a = (Calendar) c.clone();
		Calendar s = (Calendar) c.clone();
		Calendar e = (Calendar) c.clone();
		Calendar before = (Calendar) c.clone();

		t.add(Calendar.MONTH, -3);
		h.add(Calendar.MONTH, -2);
		a.add(Calendar.MONTH, -1);
		s.add(Calendar.MONTH, -4);
		e.add(Calendar.MONTH, 0);
		before.add(Calendar.MONTH, -5);

		boolean match = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, new String[] {"test", "enrollment", "initiation"}, s.getTime(), e.getTime());
		boolean match1 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, new String[] {"test", "initiation"}, s.getTime(), e.getTime());
		boolean match2 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, new String[] {"enrollment", "initiation"}, s.getTime(), e.getTime());
		boolean match3 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, new String[] {"test", "enrollment"}, s.getTime(), e.getTime());
		boolean match4 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, null, s.getTime(), e.getTime());
		boolean match5 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, null, s.getTime(), e.getTime());
		boolean match6 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, null, s.getTime(), e.getTime());
		boolean match7 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, new String[0], s.getTime(), e.getTime());
		boolean match8 = service.matchTestEnrollmentArtInitAndReturnVisitDates(null, h.getTime(), a.getTime(), null, new String[] {"test", "enrollment", "initiation"}, s.getTime(), e.getTime());
		boolean match9 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), null, null, null, new String[] {"test"}, s.getTime(), e.getTime());
		boolean match10 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), null, a.getTime(), new String[] {"test", "enrollment", "initiation"}, null, e.getTime());
		boolean match11 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), null, null, null, new String[] {"test", "enrollment", "initiation"}, s.getTime(), e.getTime());
		boolean match12 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, new String[] {"test", "enrollment", "initiation"}, new Date(), e.getTime());
		boolean match13 = service.matchTestEnrollmentArtInitAndReturnVisitDates(t.getTime(), h.getTime(), a.getTime(), null, new String[] {"test", "enrollment", "initiation"}, s.getTime(), before.getTime());

		Assert.assertTrue(match);
		Assert.assertTrue(match1);
		Assert.assertTrue(match2);
		Assert.assertTrue(match3);
		Assert.assertTrue(match4);
		Assert.assertTrue(match5);
		Assert.assertTrue(match6);
		Assert.assertTrue(match7);
		Assert.assertFalse(match8);
		Assert.assertTrue(match9);
		Assert.assertFalse(match10);
		Assert.assertFalse(match11);
		Assert.assertFalse(match11);
		Assert.assertFalse(match12);
		Assert.assertFalse(match13);
	}
}
