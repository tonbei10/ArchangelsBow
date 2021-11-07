/*
 *  This file contains the code under the MIT license.
 *  https://github.com/sinkillerj/ProjectE/blob/mc1.16.x/LICENSE
 *
 *  Original Source : https://github.com/sinkillerj/ProjectE/blob/mc1.16.x/src/main/java/moze_intel/projecte/gameObjs/entity/EntityHomingArrow.java
 */
package com.tonbei.archangelsbow;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HomingArrow extends TickArrow {

    private LivingEntity target;
    private int newTargetCooldown = 0;

    public HomingArrow(@NotNull Arrow _arrow) {
        super(_arrow);
        _arrow.setDamage(2.0);
        _arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
        _arrow.setCritical(true);
        //_arrow.setPierceLevel();
    }

    @Override
    public void tick() {
        Arrow arrow = this.getArrow();

        if (arrow.getTicksLived() > 3) {
            if (hasTarget() && (target.isDead() || arrow.isOnGround())) {
                target = null;
            }

            if (!hasTarget() && !arrow.isOnGround() && newTargetCooldown <= 0) {
                findNewTarget();
            } else {
                newTargetCooldown--;
            }

            if (hasTarget() && !arrow.isOnGround()) {
                double mX = arrow.getVelocity().getX();
                double mY = arrow.getVelocity().getY();
                double mZ = arrow.getVelocity().getZ();
                Location location = arrow.getLocation();
                double pX = location.getX();
                double pY = location.getY();
                double pZ = location.getZ();
                arrow.getWorld().spawnParticle(Particle.FLAME, pX + mX / 4.0D, pY + mY / 4.0D, pZ + mZ / 4.0D, 2, -mX / 2, -mY / 2 + 0.2D, -mZ / 2);

                Location targetLocation = target.getLocation();
                Vector arrowLoc = new Vector(pX, pY, pZ);
                Vector targetLoc = new Vector(targetLocation.getX(), targetLocation.getY() + target.getHeight() / 2, targetLocation.getZ());

                Vector lookVec = targetLoc.subtract(arrowLoc);

                Vector arrowMotion = new Vector(mX, mY, mZ);

                double theta = wrap180Radian(angleBetween(arrowMotion, lookVec));
                theta = clampAbs(theta, Math.PI / 2);

                Vector crossProduct = arrowMotion.getCrossProduct(lookVec).normalize();

                Vector adjustedLookVec = transform(crossProduct, theta, arrowMotion);
                //TODO
                Location rotationVec = location.setDirection(adjustedLookVec);
                arrow.setRotation(rotationVec.getYaw(), rotationVec.getPitch());
            }
        }
    }

    @Override
    public boolean isActive() {
        return !this.getArrow().isDead();
    }

    @Nullable
    public LivingEntity getTarget() {
        return target;
    }

    private boolean hasTarget() {
        return target != null;
    }

    private void findNewTarget() {
        List<Entity> entities = this.getArrow().getNearbyEntities(8.0, 8.0, 8.0);
        List<LivingEntity> livingEntities = entities.stream()
                                                    .filter(e -> e instanceof LivingEntity && !(e instanceof Player))
                                                    .map(e -> (LivingEntity)e)
                                                    .collect(Collectors.toList());

        if (!livingEntities.isEmpty()) {
            livingEntities.sort(Comparator.comparing(HomingArrow.this::sqrDistance, Double::compare));
            target = livingEntities.get(0);
        }

        newTargetCooldown = 5;
    }

    private double sqrDistance(@NotNull Entity entity) {
        Location al = this.getArrow().getLocation();
        Location el = entity.getLocation();
        double dx = al.getX() - el.getX();
        double dy = al.getY() - el.getY();
        double dz = al.getZ() - el.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private double angleBetween(@NotNull Vector v1, @NotNull Vector v2) {
        //TODO
        double vDot = v1.dot(v2) / (v1.length() * v2.length());
        if (vDot < -1.0) {
            vDot = -1.0;
        }
        if (vDot > 1.0) {
            vDot = 1.0;
        }
        return Math.acos(vDot);
    }

    private double wrap180Radian(double radian) {
        radian %= 2 * Math.PI;
        while (radian >= Math.PI) {
            radian -= 2 * Math.PI;
        }
        while (radian < -Math.PI) {
            radian += 2 * Math.PI;
        }
        return radian;
    }

    private double clampAbs(double param, double maxMagnitude) {
        if (Math.abs(param) > maxMagnitude) {
            if (param < 0) {
                param = -Math.abs(maxMagnitude);
            } else {
                param = Math.abs(maxMagnitude);
            }
        }
        return param;
    }

    @Contract("_, _, _ -> new")
    private @NotNull Vector transform(@NotNull Vector axis, double angle, @NotNull Vector normal) {
        double m00 = 1;
        double m01 = 0;
        double m02 = 0;

        double m10 = 0;
        double m11 = 1;
        double m12 = 0;

        double m20 = 0;
        double m21 = 0;
        double m22 = 1;

        double mag = Math.sqrt(axis.getX() * axis.getX() + axis.getY() * axis.getY() + axis.getZ() * axis.getZ());

        if (mag >= 1.0E-10) {
            mag = 1.0 / mag;
            double ax = axis.getX() * mag;
            double ay = axis.getY() * mag;
            double az = axis.getZ() * mag;

            double sinTheta = Math.sin(angle);
            double cosTheta = Math.cos(angle);
            double t = 1.0 - cosTheta;

            double xz = ax * az;
            double xy = ax * ay;
            double yz = ay * az;

            m00 = t * ax * ax + cosTheta;
            m01 = t * xy - sinTheta * az;
            m02 = t * xz + sinTheta * ay;

            m10 = t * xy + sinTheta * az;
            m11 = t * ay * ay + cosTheta;
            m12 = t * yz - sinTheta * ax;

            m20 = t * xz - sinTheta * ay;
            m21 = t * yz + sinTheta * ax;
            m22 = t * az * az + cosTheta;
        }

        return new Vector(m00 * normal.getX() + m01 * normal.getY() + m02 * normal.getZ(),
                            m10 * normal.getX() + m11 * normal.getY() + m12 * normal.getZ(),
                            m20 * normal.getX() + m21 * normal.getY() + m22 * normal.getZ());
    }
}