OpenMRS Module Appointments Backend
=================================
This repository acts as the back end for the **Bahmni Appointment Scheduling**.

## Packaging
```mvn clean package```

The output is the OMOD file:
```openmrs-module-appointments/omod/target/appointments-[VERSION].omod```


# SNAPSHOT and Release Deployments
Deployments are done from Git Actions: https://github.com/icrc/openmrs-module-appointments/actions

- [Verify on PRs](https://github.com/icrc/openmrs-module-appointments/actions/workflows/build.yml)
    - compile and run Unit tests on PRs
- [Deploy SNAPSHOT Version on Main branch](https://github.com/icrc/openmrs-module-patientgrid/actions/workflows/deploy.yml)
    - compile, unit tests and deploy snapshot versions for pushes on main branch
- [Do Release](https://github.com/icrc/openmrs-module-patientgrid/actions/workflows/release.yml)
    - manual task creating a release no SNAPHOST and moving to the next version 