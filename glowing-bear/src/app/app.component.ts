import { Component } from '@angular/core';
import { ResourceService } from './resource.service';
import { Study } from './study';

@Component({
  selector: 'my-app',
  template: `<h1>Bowing Glear</h1>
    <li *ngFor="let study of studies;">
      <span>{{study.studyId}}</span>
    </li>
  `,
  providers: [ ResourceService ]
})
export class AppComponent  { 
  studies: Study[];

  constructor(private resourceService: ResourceService) {
    this.studies = []
    this.loadStudies();
  }

  loadStudies() {
    this.resourceService.getStudies().then((studies) => {
      this.studies = studies;
    })
  }
}
