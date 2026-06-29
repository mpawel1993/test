import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-sql-bubble',
  templateUrl: './sql-bubble.component.html',
  styleUrls: ['./sql-bubble.component.css']
})
export class SqlBubbleComponent {
  @Input() sqlCode: string = 'SELECT * FROM users WHERE id = 1;'; // Domyślny placeholder
  isOpen: boolean = false;

  toggleDropdown() {
    this.isOpen = !this.isOpen;
  }
}