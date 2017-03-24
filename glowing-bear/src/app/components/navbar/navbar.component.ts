import {Component} from '@angular/core';
import {Router, NavigationEnd} from '@angular/router';

@Component({
  moduleId: module.id,
  selector: 'navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {

  isDashboard = true;
  isCohortSelection = false;
  isAnalysis = false;
  isExport = false;

  constructor(private router: Router) {
    router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        console.log('NavigationEnd:', event.urlAfterRedirects.split('/')[1]);
        this.updateNavbar(event.url.split('/')[1]);
      }
    });
  }

  updateNavbar(whichStep: string) {
    this.isDashboard = (whichStep === 'dashboard');
    this.isCohortSelection = (whichStep === 'cohort-selection');
    this.isAnalysis = (whichStep === 'analysis');
    this.isExport = (whichStep === 'export');

  }

}
