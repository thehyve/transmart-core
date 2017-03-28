import {NgModule}      from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {HttpModule}    from '@angular/http';
import {MaterialModule} from '@angular/material';
import 'hammerjs';

import {routing} from './app.routes';

import {AppComponent}  from './app.component';
import {NavbarComponent} from "./components/navbar/navbar.component";
import {DashboardComponent} from "./components/dashboard/dashboard.component";
import {DataSelectionComponent} from "./components/data-selection/data-selection.component";
import {AnalysisComponent} from "./components/analysis/analysis.component";
import {DataViewComponent} from "./components/data-view/data-view.component";
import {ExportComponent} from "./components/export/export.component";


@NgModule({
  imports: [
    BrowserModule,
    HttpModule,
    MaterialModule,
    routing
  ],
  declarations: [
    AppComponent,
    NavbarComponent,
    DashboardComponent,
    DataSelectionComponent,
    DataViewComponent,
    AnalysisComponent,
    ExportComponent
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
