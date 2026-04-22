import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AgentApprovalCardComponent } from './agent-approval-card.component';

describe('AgentApprovalCardComponent', () => {
  let component: AgentApprovalCardComponent;
  let fixture: ComponentFixture<AgentApprovalCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentApprovalCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentApprovalCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
