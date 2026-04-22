import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AgentProgressComponent } from './agent-progress.component';

describe('AgentProgressComponent', () => {
  let component: AgentProgressComponent;
  let fixture: ComponentFixture<AgentProgressComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentProgressComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentProgressComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
