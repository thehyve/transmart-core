import {Component} from '@angular/core';

import {ResourceService} from './resource.service';
import {Study} from './study';

@Component({
  moduleId: module.id,
  selector: 'my-app',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  providers: [ResourceService]
})
export class AppComponent {
  studies: Study[];

  constructor(private resourceService: ResourceService) {
    this.studies = [];
    this.loadStudies();
  }

  loadStudies() {
    this.resourceService.getStudies().then((studies) => {
      this.studies = studies;
    })
  }


}
