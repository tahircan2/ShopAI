import { Component, inject, signal, OnInit, ViewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { User } from '../../../../core/models/user.model';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [FormsModule, DatePipe, ConfirmDialogComponent],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss'
})
export class AdminUsersComponent implements OnInit {
  @ViewChild('confirmDialog') confirmDialog!: ConfirmDialogComponent;
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);

  readonly users = signal<User[]>([]);
  readonly loading = signal(true);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly page = signal(0);
  searchTerm = '';
  roleFilter = '';

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.userService.getAllUsers(this.page(), 20, this.searchTerm, this.roleFilter).subscribe({
      next: res => {
        this.users.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(Math.ceil(res.totalElements / 20));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  goPage(p: number): void { this.page.set(p); this.load(); }

  roleBadge(role: string): string {
    return role === 'ADMIN' ? 'badge badge-danger' : role === 'SELLER' ? 'badge badge-warning' : 'badge badge-primary';
  }

  changeRole(user: User, event: Event): void {
    const role = (event.target as HTMLSelectElement).value;
    this.userService.updateUserRole(user.id, role).subscribe({
      next: updated => {
        this.users.update(us => us.map(u => u.id === updated.id ? updated : u));
        this.toast.success('Rol güncellendi.');
      },
      error: () => this.toast.error('Hata oluştu.')
    });
  }

  toggleActive(user: User): void {
    this.userService.toggleUserActive(user.id).subscribe({
      next: updated => {
        this.users.update(us => us.map(u => u.id === updated.id ? updated : u));
        this.toast.success('Kullanıcı durumu güncellendi.');
      },
      error: () => this.toast.error('Hata oluştu.')
    });
  }

  async deleteUser(user: User) {
    const ok = await this.confirmDialog.open({
      title: 'Kullanıcıyı Sil',
      message: `${user.firstName} ${user.lastName} kullanıcısını silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`,
      danger: true,
      confirmText: 'Evet, Sil',
      cancelText: 'İptal'
    });

    if (ok) {
      this.userService.deleteUser(user.id).subscribe({
        next: () => {
          this.toast.success('Kullanıcı silindi.');
          this.load();
        },
        error: () => this.toast.error('Silinirken hata oluştu.')
      });
    }
  }
}
