import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from './shared/components/header/header';
import { Footer } from './shared/components/footer/footer';
import { ModalHostComponent } from './shared/components/modal/modal-host';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer, ModalHostComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
