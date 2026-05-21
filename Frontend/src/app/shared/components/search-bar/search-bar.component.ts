import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.scss',
})
export class SearchBarComponent {
  placeholder = input('Buscar…');
  value       = input('');
  valueChange = output<string>();

  clear() { this.valueChange.emit(''); }
}
