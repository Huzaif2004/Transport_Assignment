package com.transport.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "lane_quotes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"transporter_id", "lane_id"})
        }
)
public class LaneQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id")
    private Transporter transporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lane_id")
    private Lane lane;

    @Column(nullable = false)
    private Long quote;

    protected LaneQuote() {
    }

    public LaneQuote(Transporter transporter, Lane lane, Long quote) {
        this.transporter = transporter;
        this.lane = lane;
        this.quote = quote;
    }

    public Long getId() {
        return id;
    }

    public Transporter getTransporter() {
        return transporter;
    }

    public Lane getLane() {
        return lane;
    }

    public Long getQuote() {
        return quote;
    }
}
