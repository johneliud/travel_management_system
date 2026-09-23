import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from './shared/components/header/header';
import { Footer } from './shared/components/footer/footer';
import { ModalHostComponent } from './shared/components/modal/modal-host';
import { NotificationComponent } from './shared/components/notification/notification';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer, ModalHostComponent, NotificationComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
